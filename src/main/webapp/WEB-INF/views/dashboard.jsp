<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="za.co.alis.model.User, java.util.List, za.co.alis.model.Document" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
    List<Document> documents = (List<Document>) request.getAttribute("documents");
%>
<!DOCTYPE html>
<html>
<head>
    <title>ALIS Dashboard</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="dashboard-container">
        <%-- Display upload error if present --%>
        <%
            String uploadError = (String) session.getAttribute("uploadError");
            if (uploadError != null) {
        %>
            <div class="error-message" style="background: #f8d7da; color: #721c24; padding: 12px; border-radius: 6px; margin-bottom: 20px;">
                ⚠️ <%= uploadError %>
            </div>
        <%
                session.removeAttribute("uploadError");
            }
        %>

        <header>
            <h1>⚖️ ALIS Dashboard</h1>
            <div class="user-info">
                Welcome, <%= user.getFullName() %> (<%= user.getRole() %>)
                <a href="logout" class="logout-btn">Logout</a>
            </div>
        </header>

        <div class="actions">
            <a href="upload.jsp" class="btn-primary">📄 Upload New Document</a>
        </div>

        <h2>Your Documents</h2>
        <% if (documents == null || documents.isEmpty()) { %>
            <p class="empty-state">No documents uploaded yet.</p>
        <% } else { %>
            <table class="document-table">
                <thead>
                    <tr>
                        <th>Filename</th>
                        <th>Status</th>
                        <th>Uploaded</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Document doc : documents) { %>
                        <tr>
                            <td><%= doc.getFilename() %></td>
                            <td><span class="status-badge status-<%= doc.getStatus().toLowerCase() %>"><%= doc.getStatus() %></span></td>
                            <td><%= doc.getUploadedAt() %></td>
                            <td>
                                <% if ("active".equals(doc.getStatus())) { %>
                                    <a href="analysis?docId=<%= doc.getDocumentId() %>" class="btn-small">View Analysis</a>
                                <% } else if ("processing".equals(doc.getStatus())) { %>
                                    <span>Processing...</span>
                                <% } else { %>
                                    <span>Error</span>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        <% } %>
    </div>
</body>
</html>