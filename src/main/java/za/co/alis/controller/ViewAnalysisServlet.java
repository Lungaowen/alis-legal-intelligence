package za.co.alis.controller;

import za.co.alis.dao.*;
import za.co.alis.model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

public class ViewAnalysisServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("login.jsp");
            return;
        }
        int docId = Integer.parseInt(req.getParameter("docId"));
        try {
            Document doc = new DocumentDAO().findById(docId);
            List<Clause> clauses = new ClauseDAO().findByDocument(docId);
            req.setAttribute("document", doc);
            req.setAttribute("clauses", clauses);
            req.getRequestDispatcher("/WEB-INF/views/analysis.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
