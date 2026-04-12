<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>ALIS Register</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <h1>⚖️ ALIS</h1>
        <h2>Create Account</h2>
        <form method="post" action="register">
            <input type="text" name="fullName" placeholder="Full Name" required>
            <input type="email" name="email" placeholder="Email" required>
            <input type="password" name="password" placeholder="Password" required>
            <select name="role" required>
                <option value="">Select Role</option>
                <option value="attorney">Attorney</option>
                <option value="paralegal">Paralegal</option>
                <option value="user">Legal Professional</option>
            </select>
            <button type="submit">Register</button>
        </form>
        <p>Already have an account? <a href="login.jsp">Login</a></p>
        <% if (request.getAttribute("error") != null) { %>
            <p class="error"><%= request.getAttribute("error") %></p>
        <% } %>
    </div>
</body>
</html>
