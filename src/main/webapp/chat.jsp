<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="za.co.alis.model.User" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ALIS AI Chat – <%= user.getFullName() %></title>
  <link rel="stylesheet" href="css/chat.css">
</head>
<body>
  <div class="chat-container">
    <header class="chat-header">
      <h1>👨‍💻 Senior Dev Assistant</h1>
      <p>Welcome, <%= user.getFullName() %> | <a href="logout">Logout</a></p>
    </header>
    <div class="chat-messages" id="chatMessages"></div>
    <div class="chat-input-area">
      <textarea id="promptInput" placeholder="Ask a coding question..." rows="1"></textarea>
      <button id="sendBtn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </div>
  </div>
  <script src="js/chat.js"></script>
</body>
</html>
