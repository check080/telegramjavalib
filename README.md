# telegramjavalib
An easy java interface for telegram bots.
REQUIRES THE FOLLOWING LIBRARY:
[json-simple](https://code.google.com/archive/p/json-simple/)

# Tutorial
A simple application looks like the following:
```
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Master{
	
	private static TelegramBot tg;
	
	public static void main(String[] args){
		tg = new TelegramBot("..."); //Put a bot API key here
		System.out.println("Initialized.");
		
		while(true){
			JSONArray updateList = tg.update();
			if(updateList!=null){
				for(int i=0; i<updateList.size(); i++){
					
					JSONObject currentMessage = (JSONObject) ((JSONObject)updateList.get(i)).get("message");
					long chatID = (long) ((JSONObject)currentMessage.get("chat")).get("id");
					long messageID = (long) currentMessage.get("message_id");
					tg.sendReply(chatID,"ok",messageID);
					
				}
			}
		}
	}
}
```
This code will repeatedly check for messages, and replies back "ok" if one is sent.

# Documentation

`JSONArray update()` - Gets a JSON array of all messages sent to the bot since the last time update() was called. All updates that are returned are cleared from telegram's buffer.
This array is a JSON list of telegram bot update objects, read about them [here](https://core.telegram.org/bots/api#update)

`boolean sendMessage(long id, String message)` - Sends a message to the chat designated by id. If a telegram user ID is used instead, the bot will PM them.
TIP: to find user IDs and other JSON information, forward messages to @JsonDumpBot on telegram.

`boolean sendMessage(long id, String message, String[][] keyboard, boolean resize, boolean oneTime)` - Sends a message, but gives the messaged user a keyboard prompt with buttons to send as a response.
Keyboard Format: a 2D string array, row-major. e.g. new String[][]{{"a","b"},{"c","d"}} will result in a grid keyboard layout like:
```
a b
c d
```
If the resize boolean is true, it will adjust the keyboard to fit the user's screen. If the oneTime boolean is true, the keyboard will be hidden from view after a button is pressed (but it will not be removed completely, use sendMessageRemoveKeyboard)
(if resize and oneTime are not specified, they default to true and false respectively)

`boolean sendMessageRemoveKeyboard(long id, String message)` - Same as sendMessage, but it removes any keyboard prompt the user currently has open

`boolean sendReply(long id, String message, long messageID)` - Same as sendMessage, but it appears as a reply to the message denoted by messageID

# Credits
Thanks to hexdefined on github for pentesting
If you notice an error or inconvenience, please submit an issue and it will most likely be fixed
