package za.co.alis.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;

public class ApiServlet extends HttpServlet {
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private String python;

  @Override public void init(ServletConfig config) throws ServletException {
    super.init(config);
    python = System.getenv().getOrDefault("PYTHON_TRIAGE_BASE_URL","http://127.0.0.1:8000");
  }

  @Override protected void doOptions(HttpServletRequest req,HttpServletResponse resp) {
    cors(resp); resp.setStatus(204);
  }

  @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
    proxy(req,resp,"GET");
  }

  @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
    proxy(req,resp,"POST");
  }

  private void proxy(HttpServletRequest req,HttpServletResponse resp,String method) throws IOException {
    cors(resp);
    String context=req.getContextPath();
    String path=req.getRequestURI();
    if(context!=null&&!context.isEmpty()) path=path.substring(context.length());

    HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(python+path))
      .header("Content-Type",req.getHeader("Content-Type")==null?"application/json":req.getHeader("Content-Type"));

    String auth=req.getHeader("Authorization");
    if(auth!=null) builder.header("Authorization",auth);
    String tenant=req.getHeader("X-Tenant-Id");
    if(tenant!=null) builder.header("X-Tenant-Id",tenant);
    String slug=req.getHeader("X-Tenant-Slug");
    if(slug!=null) builder.header("X-Tenant-Slug",slug);

    if("POST".equals(method)) {
      byte[] body=req.getInputStream().readAllBytes();
      builder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
    } else {
      builder.GET();
    }

    try {
      HttpResponse<String> result=HTTP.send(builder.build(),HttpResponse.BodyHandlers.ofString());
      resp.setStatus(result.statusCode());
      resp.setContentType(result.headers().firstValue("content-type").orElse("application/json"));
      resp.setCharacterEncoding("UTF-8");
      resp.getWriter().write(result.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      resp.sendError(503,"Internal Python service unavailable");
    }
  }

  private static void cors(HttpServletResponse p) {
    p.setHeader("Access-Control-Allow-Origin","*");
    p.setHeader("Access-Control-Allow-Headers","Content-Type,Authorization,X-Tenant-Id,X-Tenant-Slug");
    p.setHeader("Access-Control-Allow-Methods","GET,POST,OPTIONS");
  }
}
