<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="za.co.alis.model.User, za.co.alis.model.Document, java.util.List, za.co.alis.model.Clause" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
    Document document = (Document) request.getAttribute("document");
    List<Clause> clauses = (List<Clause>) request.getAttribute("clauses");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Analysis Results - <%= document.getFilename() %></title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="analysis-container">
        <header>
            <h1>📊 Analysis Results</h1>
            <p>Document: <strong><%= document.getFilename() %></strong> | Uploaded: <%= document.getUploadedAt() %></p>
            <a href="dashboard" class="btn-small">← Back to Dashboard</a>
        </header>

        <div class="analysis-summary">
            <h2>AI Compliance Check</h2>
            <p>Clauses extracted and analyzed against South African legislation.</p>
        </div>

        <% if (clauses == null || clauses.isEmpty()) { %>
            <p class="empty-state">No clauses were identified.</p>
        <% } else { %>
            <div class="clauses-list">
                <% for (Clause clause : clauses) { %>
                    <div class="clause-card risk-<%= clause.getRiskLevel().toLowerCase() %>">
                        <div class="clause-header">
                            <span class="risk-badge"><%= clause.getRiskLevel() %></span>
                        </div>
                        <div class="clause-text">
                            <h3>Clause Text</h3>
                            <p><%= clause.getClauseText() %></p>
                        </div>
                        <div class="suggestion">
                            <h3>AI Suggestion</h3>
                            <p>Review this clause for compliance.</p>
                        </div>
                    </div>
                <% } %>
            </div>
        <% } %>
    </div>
</body>
</html>
