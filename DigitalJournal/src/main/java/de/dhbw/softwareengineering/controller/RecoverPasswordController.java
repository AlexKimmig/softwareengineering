package de.dhbw.softwareengineering.controller;

import de.dhbw.softwareengineering.model.*;
import de.dhbw.softwareengineering.model.dao.PasswordRecoveryRequestDAO;
import de.dhbw.softwareengineering.model.dao.UserDAO;
import de.dhbw.softwareengineering.utilities.Constants;
import de.dhbw.softwareengineering.utilities.Email;
import de.dhbw.softwareengineering.utilities.GeneralConfiguration;
import de.dhbw.softwareengineering.utilities.Templates;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

import java.util.UUID;

import static de.dhbw.softwareengineering.utilities.Constants.applicationContext;
import static de.dhbw.softwareengineering.utilities.Constants.prettyPrinter;

@Controller
public class RecoverPasswordController {

    /**
     * Send recovery url via email if email exists
     *
     * @param email
     * @param model
     *
     * @return
     */
    @RequestMapping(value = "/recoverpassword", method = RequestMethod.POST)
    public String index(@RequestParam("email") String email, Model model) {
        model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME, "new");
        model.addAttribute(new RegistrationUser());
        model.addAttribute(new LoginUser());
        model.addAttribute(new ContactRequest());

        applicationContext.refresh();

            UserDAO userDAO = applicationContext.getBean(UserDAO.class);
            User user       = userDAO.getUserByEMail(email);

            PasswordRecoveryRequestDAO recoveryRequestDAO = applicationContext.getBean(PasswordRecoveryRequestDAO.class);
            // Delete all requests from user
            for(PasswordRecoveryRequest recoveryRequest : recoveryRequestDAO.getAllRequestsFromUser(user.getUsername())){
                recoveryRequestDAO.deleteRequest(recoveryRequest);
                Constants.prettyPrinter.info("Deleted password recovery request from user: " + recoveryRequest.getUsername());
            }

        applicationContext.close();

        if(user != null){
            model.addAttribute("recover", "true");
            model.addAttribute("email", email);

            String uuid = UUID.randomUUID().toString();

            String url = "https://" + GeneralConfiguration.getInstance().getString("domain") + "/recoverpassword?uuid=" + uuid;
            String[] recipients = {user.getEmail()};



            PasswordRecoveryRequest recoveryRequest = new PasswordRecoveryRequest();
                                    recoveryRequest.setUsername(user.getUsername());
                                    recoveryRequest.setRecoveryUUID(uuid);
                                    recoveryRequest.setCreationDate(System.currentTimeMillis());

            prettyPrinter.info(prettyPrinter.formatObject(recoveryRequest));

            recoveryRequestDAO.addRequest(recoveryRequest);

            Email.getInstance().sendEmailSSL(recipients, "DigitalJournal: Change your password", getEmailBody(url, user.getUsername()));
        } else{
            model.addAttribute("recover", "false");
        }

        return "home";
    }


    @RequestMapping(value = "/recoverpassword", params = {"uuid"}, method = RequestMethod.GET)
    public String index(@RequestParam(value = "uuid") String uuid, Model model, HttpSession session) {

        applicationContext.refresh();
            PasswordRecoveryRequestDAO recoveryRequestDAO = applicationContext.getBean(PasswordRecoveryRequestDAO.class);
            PasswordRecoveryRequest passwordRecoveryRequest = recoveryRequestDAO.getRequestByUUID(uuid);

            prettyPrinter.info(prettyPrinter.formatObject(passwordRecoveryRequest));

            User user = null;
            if(passwordRecoveryRequest != null){
                UserDAO userDAO = applicationContext.getBean(UserDAO.class);
                user = userDAO.getUserByName(passwordRecoveryRequest.getUsername());
            }
        applicationContext.close();

        if (passwordRecoveryRequest == null) {
            model.addAttribute("status", "notFound");
            return "error";
        } else {
            if(session.getAttribute("changePasswordUser")!=null){
                session.removeAttribute("changePasswordUser");
            }
            session.setAttribute("changePasswordUser", user);
            model.addAttribute("status", "success");
            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", user.getEmail());
        }
        return "changepassword";
    }

    @RequestMapping(value = "/changepassword", method = RequestMethod.POST)
    public String index(@RequestParam("password") String password, @RequestParam("password_confirm") String password_confirm, ModelMap model, HttpSession session) {
        // security checks
        if(password.isEmpty() || password_confirm.isEmpty()){
            model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME, Constants.STATUSCODE_EMPTYFORM); return "changepassword";
        } else if(!password.matches(password_confirm)){
            model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME, Constants.STATUSCODE_PWMISSMATCH); return "changepassword";
        } else if (password.length() < 6) {
            model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME, Constants.STATUSCODE_PWTOOSHORT); return "changepassword";
        } else if (password.length() > 42) {
            model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME, Constants.STATUSCODE_PWTOOLONG); return "changepassword";
        }

        // get logged in user
        User user = (User) session.getAttribute(Constants.SESSION_CHANGEPWUSER);

        // change password of user and write to database
        if(user != null){
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setPassword(encoder.encode(password));

            applicationContext.refresh();
                UserDAO userDAO = applicationContext.getBean(UserDAO.class);
                        userDAO.updateUser(user);
            applicationContext.close();

            model.addAttribute(Constants.STATUS_ATTRIBUTE_NAME,Constants.STATUSCODE_PWCHANGESUCCESS);
        }else{
            return "error";
        }

        if(session.getAttribute(Constants.SESSION_CHANGEPWUSER)!=null){
            session.removeAttribute(Constants.SESSION_CHANGEPWUSER);
        }

        return "redirect:/";
    }

    private String getEmailBody(String url, String username) {
        String emailBody = Templates.getInstance().getTemplate(Constants.RECOVER_PASSWORD_EMAIL_TEMPLATE);
        return emailBody.replace("{$username}", username).replace("{$link}", url);
    }

}
