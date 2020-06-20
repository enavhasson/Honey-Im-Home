I pledge the highest level of ethical principles in support of academic excellence.
I ensure that all of my work reflects my own abilities and not those of someone else

question:Currently, every time we send an SMS we also show notification "sending sms: .....".
What should we add in our code-base so that when the SMS will get delivered, this notification's
 text will be changed to "sms sent: ......"?

answer :
first ,we need to declare additional PendingIntent (with identify flag- "sms_deliver")
parameter for the sendTextMessage() method,
which will send the message.
(when deliveryIntent parameter in sendTextMessage() method is not null, this is broadcast when the
message is delivered to the recipient.

second, in mpApp class(class extending Application), set registerReceiver for sms deliver with
deliverReceiver (a BroadcastReceiver class to receive the result for deliver PendingIntent)
and intent filter that filters for action "sms_deliver"
( registerReceiver(deliverReceiver, new IntentFilter(sms_deliver)) )

third, at onReceive method in deliverReceiver when the notificationID(extra parameter in the intent)
is accordingly to the notification "sending sms:..." ,
we will change the notification message to "SMS sent:..." .


in another words,we need to add for the PendingIntent's (for delivery in sendTextMessage() method)
extras the notificationID (for sending sms)
and the broadcast-receiver (for deliveryIntent) will update the notification's message only
when notificationID is correct.