package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;




public class RegistrationCreatedConsumer {

    private final EmailService emailService;

    public RegistrationCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    public void consume(String email) {
        throw new UnsupportedOperationException();
    }

}
