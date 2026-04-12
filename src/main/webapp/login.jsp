<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>ALIS Login</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <h1>⚖️ ALIS</h1>
        <h2>Legal Intelligence System</h2>
        <form method="post" action="login">
            <input type="email" name="email" placeholder="Email" required>
            <input type="password" name="password" placeholder="Password" required>
            <button type="submit">Login</button>
        </form>
        <p>New user? <a href="register.jsp">Create account</a></p>
        <% if (request.getParameter("registered") != null) { %>
            <p class="success">Registration successful! Please login.</p>
        <% } %>
        <% if (request.getAttribute("error") != null) { %>
            <p class="error"><%= request.getAttribute("error") %></p>
        <% } %>
    </div>
</body>
</html>
