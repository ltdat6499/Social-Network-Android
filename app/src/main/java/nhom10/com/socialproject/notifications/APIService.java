package nhom10.com.socialproject.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA58lKeJk:APA91bHlyoqxMjX0UlK4MpPnrZlbgRjA2JptfHxcXl6yK_0-iGl7woQvw6UvFPogFjFy3RVn_KM66cgf1JLlnEMQErk_BNmG6q5fidYL36s3ASv_262LnJM49JmwZ5e_to14yUKmp85H"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
