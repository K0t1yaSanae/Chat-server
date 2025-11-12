package net.kotiyasanae.chatserver.encryption;

import net.kotiyasanae.chatserver.model.Message;
import org.eclipse.jetty.server.session.Session;

public class TestEncryption {

    private String outMessageContent;

    public Message testEncryption(Message inputMessage) {
        Message outMessage = new Message();

        outMessage.setSender(inputMessage.getSender());
        outMessage.setContent(inputMessage.getContent());
        outMessage.setTimestamp(inputMessage.getTimestamp());
        outMessage.setType(inputMessage.getType());

        String outputContent;
        outputContent = inputMessage.getContent()+"TestEncryption";
        outMessage.setContent(outputContent);
        outMessageContent=outputContent;
        return outMessage;
    }

    public String getOutMessageContent() {
        return outMessageContent;
    }
}
