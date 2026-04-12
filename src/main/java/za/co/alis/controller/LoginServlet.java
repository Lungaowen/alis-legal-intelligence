package za.co.alis.controller;

import za.co.alis.dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;

public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String pass = req.getParameter("password");
        try {
            if (new UserDAO().validateLogin(email, pass)) {
                HttpSession session = req.getSession();
                session.setAttribute("user", new UserDAO().findByEmail(email));
                resp.sendRedirect("dashboard");
            } else {
                req.setAttribute("error", "Invalid credentials");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
