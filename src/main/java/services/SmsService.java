package services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {

    private static final String ACCOUNT_SID = "AC626df3e3f1938ecea96e32d1035bd1f4";
    private static final String AUTH_TOKEN = "6dd5ecfd73987ac530742ef7b576984e";
    private static final String FROM_NUMBER = "+16502784608";

    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public static void sendSms(String to, String message) {

        try{

            if(!to.startsWith("+")){
                to = "+216" + to;
            }

            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(FROM_NUMBER),
                    message
            ).create();

            System.out.println("SMS sent");

        }catch(Exception e){
            e.printStackTrace();
        }
    }


}
