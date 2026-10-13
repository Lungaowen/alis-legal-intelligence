import base64, hashlib, hmac, json, logging, os, secrets, uuid
from datetime import datetime, timezone

import httpx, redis
from celery import Celery
from fastapi import FastAPI, HTTPException, Request, Response
from pydantic import BaseModel, Field

from config import config

app=FastAPI(title="ALIS Internal Core + Triage")
rds=redis.Redis.from_url(config.REDIS_URL,decode_responses=True)
celery_app=Celery("alis_triage",broker=config.REDIS_URL,backend=config.REDIS_URL)
GROQ="https://api.groq.com/openai/v1/chat/completions"
log=logging.getLogger(__name__)
SESSION_TTL=7*24*60*60

class RegisterRequest(BaseModel):
    fullName:str
    email:str
    password:str=Field(min_length=8)
    role:str="USER"

class LoginRequest(BaseModel):
    email:str
    password:str

class MatterRequest(BaseModel):
    title:str
    description:str=""

class TriageRequest(BaseModel):
    workflow:str="general"
    narrative:str=Field(min_length=10,max_length=20000)
    jurisdiction:str="South Africa"
    document_context:list[str]=Field(default_factory=list)

def now():
    return datetime.now(timezone.utc).isoformat()

def hash_password(password:str,salt:bytes=None):
    salt=salt or secrets.token_bytes(16)
    digest=hashlib.pbkdf2_hmac("sha256",password.encode(),salt,310000)
    return base64.urlsafe_b64encode(salt).decode().rstrip("=")+":"+base64.urlsafe_b64encode(digest).decode().rstrip("=")

def verify_password(password:str,stored:str):
    try:
        s,d=stored.split(":",1)
        salt=base64.urlsafe_b64decode(s+"="*((4-len(s)%4)%4))
        expected=base64.urlsafe_b64decode(d+"="*((4-len(d)%4)%4))
        actual=hashlib.pbkdf2_hmac("sha256",password.encode(),salt,310000)
        return hmac.compare_digest(actual,expected)
    except Exception:
        return False

def user_id_for(email):
    return rds.get("alis:user:email:"+email.lower())

def get_user(uid):
    raw=rds.get("alis:user:"+str(uid))
    return json.loads(raw) if raw else None

def session_user(request:Request):
    header=request.headers.get("Authorization","")
    if not header.startswith("Bearer "):
        raise HTTPException(401,"Authentication required")
    token=header[7:].strip()
    raw=rds.get("alis:session:"+token)
    if not raw:
        raise HTTPException(401,"Session expired or invalid")
    return json.loads(raw)

def matter(uid,matter_id):
    raw=rds.get("alis:matter:"+str(matter_id))
    if not raw:
        raise HTTPException(404,"Matter not found")
    item=json.loads(raw)
    if int(item["userId"])!=int(uid):
        raise HTTPException(404,"Matter not found")
    return item

def set_triage(task_id,status,message,result=None,error=None):
    rds.setex("triage:"+task_id,86400,json.dumps({
        "task_id":task_id,"status":status,"message":message,
        "result":result,"error":error,"updatedAt":now()
    }))

@app.post("/api/auth/register")
def register(q:RegisterRequest):
    email=q.email.strip().lower()
    if rds.exists("alis:user:email:"+email):
        return Response(content=json.dumps({"message":"An account with that email already exists"}),status_code=400,media_type="application/json")
    uid=rds.incr("alis:user:next")
    user={"clientId":uid,"email":email,"fullName":q.fullName.strip(),"role":q.role}
    rds.set("alis:user:"+str(uid),json.dumps({**user,"passwordHash":hash_password(q.password)}))
    rds.set("alis:user:email:"+email,str(uid))
    return {"success":True}

@app.post("/api/auth/login")
def login(q:LoginRequest):
    email=q.email.strip().lower()
    uid=user_id_for(email)
    user=get_user(uid) if uid else None
    if not user or not verify_password(q.password,user.get("passwordHash","")):
        raise HTTPException(401,"Invalid email or password")
    token=secrets.token_urlsafe(48)
    public={k:user[k] for k in ("clientId","email","fullName","role")}
    rds.setex("alis:session:"+token,SESSION_TTL,json.dumps(public))
    return {"success":True,**public,"token":token}

@app.get("/api/v1/matters")
def list_matters(request:Request):
    user=session_user(request)
    ids=rds.smembers("alis:user:"+str(user["clientId"])+":matters")
    items=[]
    for item_id in ids:
        raw=rds.get("alis:matter:"+str(item_id))
        if raw: items.append(json.loads(raw))
    items.sort(key=lambda x:int(x["id"]),reverse=True)
    return items

@app.post("/api/v1/matters")
def create_matter(request:Request,q:MatterRequest):
    user=session_user(request)
    mid=rds.incr("alis:matter:next")
    item={"id":mid,"title":q.title.strip(),"description":q.description.strip(),"status":"CREATED",
          "createdAt":now(),"updatedAt":now(),"userId":user["clientId"],"workflow":None,
          "narrative":None,"taskId":None}
    rds.set("alis:matter:"+str(mid),json.dumps(item))
    rds.sadd("alis:user:"+str(user["clientId"])+":matters",str(mid))
    return {k:item[k] for k in ("id","title","description","status","createdAt","updatedAt")}

def build_analysis(workflow,narrative,context):
    if not config.GROQ_API_KEY:
        raise RuntimeError("GROQ_API_KEY is not configured on the Render service")
    prompt=f"""You are ALIS, a South African legal-information triage assistant. You are not a lawyer.
Do not invent facts, dates, parties, legal conclusions, quotations or outcomes. Return ONLY valid JSON.
Workflow: {workflow}
Jurisdiction: South Africa
Narrative: {narrative}
Source excerpts: {json.dumps(context[:10],ensure_ascii=False)}
Return exactly: {{"matter_summary":"string","parties_involved":["string"],"timeline_of_events":[{{"date":"YYYY-MM-DD or Unknown","event":"string"}}],"extracted_clauses":[{{"issue_type":"string","exact_quote":"exact source wording only","business_impact":"string"}}],"identified_gaps":["string"],"recommended_next_steps":["string"]}}
Only quote text found verbatim in Source excerpts."""
    x=httpx.post(GROQ,headers={"Authorization":"Bearer "+config.GROQ_API_KEY,"Content-Type":"application/json"},
      json={"model":config.GROQ_MODEL,"messages":[{"role":"user","content":prompt}],"response_format":{"type":"json_object"},"temperature":0.2},timeout=120)
    x.raise_for_status()
    return json.loads(x.json()["choices"][0]["message"]["content"])

@celery_app.task(name="triage_situation")
def triage_task(task_id,uid,matter_id,workflow,narrative,context):
    try:
        set_triage(task_id,"PROCESSING","ALIS is organising the supplied information.")
        result=build_analysis(workflow,narrative,context)
        set_triage(task_id,"COMPLETED","Situation map ready for review.",result)
        return result
    except Exception as e:
        log.exception("triage %s failed",task_id)
        set_triage(task_id,"FAILED","The situation map could not be completed.",error=f"{type(e).__name__}: {e}")
        raise

@app.post("/api/v1/matters/{matter_id}/triage")
def start_triage(matter_id:int,request:Request,q:TriageRequest):
    user=session_user(request)
    item=matter(user["clientId"],matter_id)
    task_id=str(uuid.uuid4())
    set_triage(task_id,"QUEUED","Your situation map has been queued.")
    item.update({"workflow":q.workflow,"narrative":q.narrative,"taskId":task_id,"status":"PROCESSING","updatedAt":now()})
    rds.set("alis:matter:"+str(matter_id),json.dumps(item))
    triage_task.delay(task_id,user["clientId"],matter_id,q.workflow,q.narrative,q.document_context)
    return {"task_id":task_id,"status":"QUEUED","message":"Your situation map has been queued."}

def triage_for_user(request,matter_id,task_id):
    user=session_user(request)
    item=matter(user["clientId"],matter_id)
    expected=item.get("taskId")
    if not expected or expected!=task_id:
        raise HTTPException(404,"Triage task not found")
    raw=rds.get("triage:"+task_id)
    if not raw: raise HTTPException(404,"Triage task not found")
    return json.loads(raw)

@app.get("/api/v1/matters/{matter_id}/triage/{task_id}")
def triage_status(matter_id:int,task_id:str,request:Request):
    return triage_for_user(request,matter_id,task_id)

@app.get("/api/v1/matters/{matter_id}/triage/latest")
def triage_latest(matter_id:int,request:Request):
    user=session_user(request)
    item=matter(user["clientId"],matter_id)
    task_id=item.get("taskId")
    if not task_id: raise HTTPException(404,"No triage job exists for this matter")
    return triage_for_user(request,matter_id,task_id)
