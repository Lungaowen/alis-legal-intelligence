import json, logging, uuid
from datetime import datetime
import httpx, redis
from celery import Celery
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from config import config

app=FastAPI(title="ALIS Internal Triage")
rds=redis.Redis.from_url(config.REDIS_URL,decode_responses=True)
celery_app=Celery("alis_triage",broker=config.REDIS_URL,backend=config.REDIS_URL)
GROQ="https://api.groq.com/openai/v1/chat/completions"
log=logging.getLogger(__name__)

class Req(BaseModel):
    workflow:str="general"
    narrative:str=Field(min_length=10,max_length=20000)
    document_context:list[str]=Field(default_factory=list)

def status(t,s,m,result=None,error=None):
    rds.setex("triage:"+t,86400,json.dumps({"task_id":t,"status":s,"message":m,"result":result,"error":error,"updatedAt":datetime.utcnow().isoformat()+"Z"}))

def analyse(workflow,narrative,ctx):
    if not config.GROQ_API_KEY: raise RuntimeError("GROQ_API_KEY is not configured on Render")
    prompt=f"""You are ALIS, a South African legal-information triage assistant. You are not a lawyer.
Do not invent facts, dates, parties, legal conclusions, quotations or outcomes. Return ONLY JSON.
Workflow: {workflow}
Jurisdiction: South Africa
Narrative: {narrative}
Source excerpts: {json.dumps(ctx[:10],ensure_ascii=False)}
Return exactly: {{"matter_summary":"string","parties_involved":["string"],"timeline_of_events":[{{"date":"YYYY-MM-DD or Unknown","event":"string"}}],"extracted_clauses":[{{"issue_type":"string","exact_quote":"exact source wording only","business_impact":"string"}}],"identified_gaps":["string"],"recommended_next_steps":["string"]}}
Only quote text found verbatim in Source excerpts."""
    x=httpx.post(GROQ,headers={"Authorization":"Bearer "+config.GROQ_API_KEY,"Content-Type":"application/json"},
      json={"model":config.GROQ_MODEL,"messages":[{"role":"user","content":prompt}],"response_format":{"type":"json_object"},"temperature":0.2},timeout=120)
    x.raise_for_status()
    return json.loads(x.json()["choices"][0]["message"]["content"])

@celery_app.task(name="triage_situation")
def triage_task(t,w,n,c):
    try:
        status(t,"PROCESSING","ALIS is organising the supplied information.")
        result=analyse(w,n,c)
        status(t,"COMPLETED","Situation map ready for review.",result)
        return result
    except Exception as e:
        log.exception("triage %s failed",t)
        status(t,"FAILED","The situation map could not be completed.",error=f"{type(e).__name__}: {e}")
        raise

@app.post("/api/triage")
def queue(q:Req):
    t=str(uuid.uuid4()); status(t,"QUEUED","Your situation map has been queued.")
    triage_task.delay(t,q.workflow,q.narrative,q.document_context)
    return {"task_id":t,"status":"QUEUED","message":"Your situation map has been queued."}

@app.get("/api/triage/{task_id}")
def get_status(task_id:str):
    x=rds.get("triage:"+task_id)
    if not x: raise HTTPException(status_code=404,detail="Triage task not found")
    return json.loads(x)
