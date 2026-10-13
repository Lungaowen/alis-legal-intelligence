package za.co.alis.api;

import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import za.co.alis.dao.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class ApiServlet extends HttpServlet {
  private static final Gson JSON = new Gson();
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private String secret;
  private String python;

  public void init(ServletConfig c) throws ServletException {
    super.init(c);
    secret = required("ALIS_JWT_SECRET");
    python = System.getenv().getOrDefault("PYTHON_TRIAGE_BASE_URL","http://127.0.0.1:8000");
    try (Connection db=DBConnection.getConnection(); Statement s=db.createStatement()) {
      s.execute("""
        CREATE TABLE IF NOT EXISTS users(
          user_id SERIAL PRIMARY KEY,
          full_name VARCHAR(255) NOT NULL,
          email VARCHAR(320) NOT NULL UNIQUE,
          password_hash VARCHAR(255) NOT NULL,
          role VARCHAR(64) NOT NULL DEFAULT 'USER',
          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS matters(
          id SERIAL PRIMARY KEY,
          user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
          title VARCHAR(255) NOT NULL,
          description TEXT,
          workflow VARCHAR(64),
          narrative TEXT,
          task_id VARCHAR(128),
          status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX IF NOT EXISTS idx_matters_user ON matters(user_id);
        CREATE INDEX IF NOT EXISTS idx_matters_task ON matters(task_id);
      """);
    } catch(SQLException e) { throw new ServletException("Database initialization failed",e); }
  }

  private static String required(String k) throws ServletException {
    String v=System.getenv(k); if(v==null||v.isBlank()) throw new ServletException(k+" is required"); return v;
  }

  protected void doOptions(HttpServletRequest r,HttpServletResponse p){ cors(p); p.setStatus(204); }

  protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
    cors(p); String path=path(r);
    try {
      if(path.equals("api/auth/register")) { register(r,p); return; }
      if(path.equals("api/auth/login")) { login(r,p); return; }
      if(path.equals("api/v1/matters")) { createMatter(r,p,auth(r)); return; }
      if(path.matches("api/v1/matters/[0-9]+/triage")) {
        int id=Integer.parseInt(path.split("/")[3]); startTriage(r,p,auth(r),id); return;
      }
      err(p,404,"Not found");
    } catch(Unauthorized e){ err(p,401,e.getMessage()); }
      catch(BadRequest e){ err(p,400,e.getMessage()); }
      catch(Exception e){ e.printStackTrace(); err(p,500,"ALIS API error"); }
  }

  protected void doGet(HttpServletRequest r,HttpServletResponse p) throws IOException {
    cors(p); String path=path(r);
    try {
      if(path.equals("api/v1/matters")) { listMatters(p,auth(r)); return; }
      if(path.startsWith("api/v1/matters/")) { nestedGet(r,p); return; }
      err(p,404,"Not found");
    } catch(Unauthorized e){ err(p,401,e.getMessage()); }
      catch(Exception e){ e.printStackTrace(); err(p,500,"ALIS API error"); }
  }

  private String path(HttpServletRequest r){
    String x=r.getRequestURI(), c=r.getContextPath();
    if(c!=null&&!c.isEmpty()) x=x.substring(c.length());
    return x.startsWith("/")?x.substring(1):x;
  }

  private int auth(HttpServletRequest r) throws Exception {
    String h=r.getHeader("Authorization");
    if(h==null||!h.startsWith("Bearer ")) throw new Unauthorized("Authentication required");
    String[] a=h.substring(7).split("\\.",2);
    if(a.length!=2) throw new Unauthorized("Invalid session");
    byte[] payload=Base64.getUrlDecoder().decode(a[0]);
    if(!MessageDigest.isEqual(Base64.getUrlDecoder().decode(a[1]),hmac(payload))) throw new Unauthorized("Invalid session");
    String[] f=new String(payload,StandardCharsets.UTF_8).split("\\|",4);
    if(f.length!=4||Instant.now().getEpochSecond()-Long.parseLong(f[2])>604800) throw new Unauthorized("Session expired");
    int uid=Integer.parseInt(f[0]);
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement("select 1 from users where user_id=? and email=?")){
      q.setInt(1,uid); q.setString(2,f[1]); if(!q.executeQuery().next()) throw new Unauthorized("Session is no longer valid");
    }
    return uid;
  }

  private void register(HttpServletRequest r,HttpServletResponse p) throws Exception{
    JsonObject b=read(r); String name=str(b,"fullName"), email=str(b,"email").toLowerCase(), pass=str(b,"password");
    if(name.isBlank()||email.isBlank()||pass.length()<8) throw new BadRequest("Name, email and password (8+ characters) are required");
    String role=b.has("role")?b.get("role").getAsString():"USER";
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement("insert into users(full_name,email,password_hash,role) values(?,?,?,?)")){
      q.setString(1,name); q.setString(2,email); q.setString(3,BCrypt.hashpw(pass,BCrypt.gensalt(12))); q.setString(4,role); q.executeUpdate();
    } catch(SQLException e){ if("23505".equals(e.getSQLState())) throw new BadRequest("An account with that email already exists"); throw e; }
    send(p,201,obj("success",true));
  }

  private void login(HttpServletRequest r,HttpServletResponse p) throws Exception{
    JsonObject b=read(r); String email=str(b,"email").toLowerCase(), pass=str(b,"password");
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement("select user_id,full_name,email,password_hash,role from users where email=?")){
      q.setString(1,email); ResultSet s=q.executeQuery();
      if(!s.next()||!BCrypt.checkpw(pass,s.getString("password_hash"))){ err(p,401,"Invalid email or password"); return; }
      JsonObject o=new JsonObject(); o.addProperty("success",true); o.addProperty("clientId",s.getInt(1)); o.addProperty("email",s.getString(3));
      o.addProperty("fullName",s.getString(2)); o.addProperty("role",s.getString(5)); o.addProperty("token",token(s.getInt(1),s.getString(3))); send(p,200,o);
    }
  }

  private void createMatter(HttpServletRequest r,HttpServletResponse p,int uid) throws Exception{
    JsonObject b=read(r); String title=str(b,"title"), desc=str(b,"description");
    if(title.isBlank()) throw new BadRequest("Matter title is required");
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement(
      "insert into matters(user_id,title,description) values(?,?,?) returning id,title,description,status,created_at,updated_at")){
      q.setInt(1,uid); q.setString(2,title); q.setString(3,desc); ResultSet s=q.executeQuery(); s.next(); send(p,201,matter(s));
    }
  }

  private void listMatters(HttpServletResponse p,int uid) throws Exception{
    JsonArray a=new JsonArray();
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement(
      "select id,title,description,status,created_at,updated_at from matters where user_id=? order by id desc")){
      q.setInt(1,uid); ResultSet s=q.executeQuery(); while(s.next()) a.add(matter(s));
    } send(p,200,a);
  }

  private void nestedGet(HttpServletRequest r,HttpServletResponse p) throws Exception{
    String[] x=path(r).substring("api/v1/matters/".length()).split("/");
    if(x.length!=3||!"triage".equals(x[1])) { err(p,404,"Not found"); return; }
    int id=Integer.parseInt(x[0]), uid=auth(r); owner(uid,id);
    String task=x[2].equals("latest")?taskId(id):x[2];
    if(task==null){ err(p,404,"No triage job exists for this matter"); return; }
    proxy(p,"GET","/api/triage/"+task,null);
  }

  private void startTriage(HttpServletRequest r,HttpServletResponse p,int uid,int id) throws Exception{
    owner(uid,id); JsonObject b=read(r); String narrative=str(b,"narrative");
    if(narrative.length()<10) throw new BadRequest("Please provide at least 10 characters describing the situation");
    JsonObject q=new JsonObject(); q.addProperty("workflow",str(b,"workflow")); q.addProperty("narrative",narrative);
    q.add("document_context",b.has("document_context")?b.get("document_context"):new JsonArray());
    JsonObject queued=proxy(p,"POST","/api/triage",q);
    if(queued.has("task_id")) try(Connection db=DBConnection.getConnection(); PreparedStatement u=db.prepareStatement(
      "update matters set workflow=?,narrative=?,task_id=?,status='PROCESSING',updated_at=current_timestamp where id=?")){
      u.setString(1,str(b,"workflow")); u.setString(2,narrative); u.setString(3,queued.get("task_id").getAsString()); u.setInt(4,id); u.executeUpdate();
    }
  }

  private JsonObject proxy(HttpServletResponse p,String method,String path,JsonObject body) throws IOException{
    HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(python+path)).header("Content-Type","application/json");
    if("POST".equals(method)) b.POST(HttpRequest.BodyPublishers.ofString(body.toString())); else b.GET();
    try{ HttpResponse<String> x=HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString()); p.setStatus(x.statusCode()); p.setContentType("application/json"); p.getWriter().write(x.body());
      JsonElement e=JsonParser.parseString(x.body()); return e.isJsonObject()?e.getAsJsonObject():new JsonObject();
    } catch(InterruptedException e){ Thread.currentThread().interrupt(); throw new IOException(e); }
  }

  private void owner(int uid,int id) throws Exception{
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement("select 1 from matters where id=? and user_id=?")){
      q.setInt(1,id); q.setInt(2,uid); if(!q.executeQuery().next()) throw new Unauthorized("Matter not found");
    }
  }

  private String taskId(int id) throws SQLException{
    try(Connection db=DBConnection.getConnection(); PreparedStatement q=db.prepareStatement("select task_id from matters where id=?")){
      q.setInt(1,id); ResultSet s=q.executeQuery(); return s.next()?s.getString(1):null;
    }
  }

  private static JsonObject matter(ResultSet s)throws SQLException{
    JsonObject o=new JsonObject(); o.addProperty("id",s.getInt(1)); o.addProperty("title",s.getString(2)); o.addProperty("description",s.getString(3));
    o.addProperty("status",s.getString(4)); o.addProperty("createdAt",s.getTimestamp(5).toInstant().toString()); o.addProperty("updatedAt",s.getTimestamp(6).toInstant().toString()); return o;
  }

  private String token(int uid,String email)throws Exception{
    String raw=uid+"|"+email+"|"+Instant.now().getEpochSecond()+"|"+UUID.randomUUID(); byte[] b=raw.getBytes(StandardCharsets.UTF_8);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b)+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(b));
  }
  private byte[] hmac(byte[] b)throws Exception{ Mac m=Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")); return m.doFinal(b); }
  private static JsonObject read(HttpServletRequest r)throws IOException{ StringBuilder b=new StringBuilder(); try(BufferedReader x=r.getReader()){String l;while((l=x.readLine())!=null)b.append(l);} return b.isEmpty()?new JsonObject():JsonParser.parseString(b.toString()).getAsJsonObject(); }
  private static String str(JsonObject b,String k){return b.has(k)&&!b.get(k).isJsonNull()?b.get(k).getAsString().trim():"";}
  private static JsonObject obj(String k,boolean v){JsonObject o=new JsonObject();o.addProperty(k,v);return o;}
  private static void send(HttpServletResponse p,int s,Object v)throws IOException{p.setStatus(s);p.setContentType("application/json");p.getWriter().write(JSON.toJson(v));}
  private static void err(HttpServletResponse p,int s,String m)throws IOException{JsonObject o=new JsonObject();o.addProperty("message",m);send(p,s,o);}
  private static void cors(HttpServletResponse p){p.setHeader("Access-Control-Allow-Origin","*");p.setHeader("Access-Control-Allow-Headers","Content-Type,Authorization,X-Tenant-Id,X-Tenant-Slug");p.setHeader("Access-Control-Allow-Methods","GET,POST,OPTIONS");}
  private static class Unauthorized extends Exception{Unauthorized(String m){super(m);}}
  private static class BadRequest extends Exception{BadRequest(String m){super(m);}}
}
