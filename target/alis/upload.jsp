<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="za.co.alis.model.User" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>Upload Document - ALIS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <h1>📤 Upload Legal Document</h1>
        <p>Supported format: .txt</p>
        <form method="post" action="upload" enctype="multipart/form-data">
            <div class="file-upload-area">
                <input type="file" name="document" accept=".txt,text/plain" required>
            </div>
            <button type="submit" class="btn-primary">Analyze with AI</button>
        </form>
        <p><a href="dashboard">← Back to Dashboard</a></p>
        <% if (request.getAttribute("error") != null) { %>
            <p class="error"><%= request.getAttribute("error") %></p>
        <% } %>
    </div>
</body>
</html>
