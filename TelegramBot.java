import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.lang.Math;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.net.SocketTimeoutException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.IOException;
//NEEDS JSONSIMPLE:
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class TelegramBot{
	private final static int TELEGRAM_MAX_MESSAGE_SIZE = 4096; //kind of arbitrary, this is the default number of characters the message-vector holds, take this down a bit if you update() a lot
	private final static int MAX_MESSAGE_UPDATES = 99; //this is per update() call, must be from 1 to 99
	private final static int CHARBUF_SIZE = 1024;
    private final static String TELEGRAM_URL = "https://api.telegram.org/bot";
	private final static String USERAGENT_HEADER = "Test project by yourname"; //Not required by telegram, but might be in the future

    private String accessURL;
    private long lastUpdate;

    public TelegramBot(String APIKey){
        this.accessURL = TELEGRAM_URL+APIKey+"/";
        this.lastUpdate=0;
    }
	
	//PUBLIC INSTANCE FUNCTIONS

    /**
    * Gets all the messages sent to the bot since last time update() was called
    * @return The JSON data obtained from the site, or null on failure
    */
    public JSONArray update(){
		try{
			JSONObject jsonObj = (JSONObject) getJSONFromURL(accessURL+"getUpdates","offset="+this.lastUpdate+"&limit="+(MAX_MESSAGE_UPDATES+1));
			if(jsonObj == null){ return null; }
			else if((boolean) jsonObj.get("ok") == false){
				System.out.println("[DEBUG] Website returned not ok.");
				return null;
			}
		
			//Clear updates that have been received
			JSONArray objData = (JSONArray) jsonObj.get("result");
			int objDataSize = objData.size();
			if(objDataSize==0){ //if there's no new updates
				return objData;
			}
			else if(objData.size()>MAX_MESSAGE_UPDATES){ //someone's spamming
				this.lastUpdate = (long) ((JSONObject) objData.get(objData.size()-1)).get("update_id") + 1; //The ID of the latest message plus 1
				System.out.println("[DEBUG] Buffer reset. Possible spam.");
				return this.update(); //recurse until buffer is clear
			}
			else{
				this.lastUpdate = (long) ((JSONObject) objData.get(objData.size()-1)).get("update_id") + 1; //The ID of the latest message plus 1
				return objData;
			}
		}
		catch(ClassCastException e){ //URL didn't return an array or objData doesn't contain JSONObjects
			System.out.println("[DEBUG] Site did not return the expected JSON array.");
			return null;
		}
		catch(NullPointerException e){ //no "update_id"
			System.out.println("[DEBUG] Site JSON was not formatted as expected.");
			return null;
		}
    }

    /**
    * Sends a message to a chat.
    * @param id The chat ID
    * @param message The text to send
    * @return True if success, false if failure
    */
    public boolean sendMessage(long id, String message){
		return sendMessage(id,message,"");
    }

    /**
    *  Sends a message to the chat ID with a keyboard. If sent to a group, all members will be given a keyboard.
    * @param id The chat ID
    * @param message The text to send
    * @param keyboard a 2D string array for the keyboard buttons; e.g. {{"topleft","topright"},{"bottomleft","bottomright"}}
	* @param resize true if every button should be the same height, false if the keyboard should be the default height
	* @param oneTime true if the keyboard hides itself after a button press. Use sendMessageRemoveKeyboard to fully remove it.
    * @return True if success, false if failure
    */
    public boolean sendMessage(long id, String message, String[][] keyboard, boolean resize, boolean oneTime){
		try{
			int rowNum = keyboard.length;
			String[] rows = new String[rowNum];
			for(int i=0;i<rowNum;i++){
				int buttonNum = keyboard[i].length;
				String[] buttons = new String[buttonNum];
				for(int j=0;j<buttonNum;j++)
					buttons[j] = new String("\""+URLEncoder.encode(keyboard[i][j],"UTF-8")+"\"");
				rows[i] = new String("["+String.join(",",buttons)+"]");
			}
			String args = "&reply_markup={\"keyboard\":["+String.join(",",rows)+"]";
			if(resize) args += ",\"resize_keyboard\":true";
			if(oneTime) args += ",\"one_time_keyboard\":true";
			args += "}";
			return sendMessage(id,message,args);
		}
		catch(UnsupportedEncodingException e){
			System.out.println("Unable to UTF-8 encode the given keyboard buttons.");
			return false;
		}
    }
	
	/**
    *  Sends a message to the chat ID with a keyboard. If sent to a group, all members will be given a keyboard.
    * @param id The chat ID
    * @param message The text to send
    * @param keyboard a 2D string array for the keyboard buttons; e.g. {{"topleft","topright"},{"bottomleft","bottomright"}}
    * @return True if success, false if failure
    */
	public boolean sendMessage(long id, String message, String[][] keyboard){
		return sendMessage(id,message,keyboard,true,false);
	}
	
	public boolean sendMessageRemoveKeyboard(long id, String message){
		String args = "&reply_markup={\"remove_keyboard\":true}";
		return sendMessage(id,message,args);
	}
	
	public boolean sendReply(long chatID, String message, long messageID){
		String args = "&reply_to_message_id="+messageID;
		return sendMessage(chatID,message,args);
	}
	
	//PRIVATE INSTANCE FUNCTIONS
	
	private boolean sendMessage(long id, String message, String args){
		try{
			boolean success = true;
			int messageLength = message.length();
			int numMessages = messageLength/TELEGRAM_MAX_MESSAGE_SIZE+1;
			for(int i=0;i<numMessages;i++){
				String messagePiece = message.substring(i*TELEGRAM_MAX_MESSAGE_SIZE,Math.min((i+1)*TELEGRAM_MAX_MESSAGE_SIZE,messageLength));
				JSONObject objData = (JSONObject) getJSONFromURL(accessURL + "sendMessage","chat_id=" + id + "&text=" + URLEncoder.encode(messagePiece,"UTF-8") + args);
				if((boolean) objData.get("ok")){
					System.out.println("TO "+id+": "+message);
				}
				else success = false;
			}
			if(success){
				return true;
			}
			else return false;
		}
		catch(NullPointerException e){ return false; } //no "ok"
		catch(UnsupportedEncodingException e){
			System.out.println("[DEBUG] Unable to UTF-8 encode: "+message);
			return false;
		}
	}
	
	//PRIVATE STATIC FUNCTIONS
	
	private static String cleanInput(byte[] charBuf, int bufLength){
		if(bufLength>charBuf.length) return "";
		
		StringBuilder builder = new StringBuilder();
		for(int i=0;i<bufLength;i++){
			if(charBuf[i]>=32 && charBuf[i]<=126){
				builder.append((char) charBuf[i]);
			}
		}
		return builder.toString();
	}
	
	public static String getStringFromPOSTURL(URL siteURL,String args){
		try{
			HttpsURLConnection con = (HttpsURLConnection) siteURL.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent","Test project by check080");
			
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(args);
			wr.flush();
			wr.close();
			
			return getStringFromConnection(con);
		}
		catch(IOException e){
			System.out.println("[DEBUG] Site not found. Is your URL correct?");
			return null;
		}
	}
	
	public static String getStringFromGETURL(URL siteURL){
		try{
			HttpsURLConnection con = (HttpsURLConnection) siteURL.openConnection();
			con.setRequestProperty("User-Agent",USERAGENT_HEADER);
			
			return getStringFromConnection(con);
		}
		catch(IOException e){
			System.out.println("[DEBUG] Site not found. Is your URL correct?");
			return null;
		}
	}
	
	private static String getStringFromConnection(HttpsURLConnection con){
		try{
			//con.getResponseCode(); to check if it's even open i guess
			
			String contentType = con.getContentType();
			String encoding;
			try{
				encoding = contentType.substring(contentType.indexOf('=')+1);
				encoding = encoding == null ? "UTF-8" : encoding;
			}
			catch(IndexOutOfBoundsException e){
				encoding = "UTF-8";
			}
			
			InputStream in = con.getInputStream();
			byte[] charBuf = new byte[CHARBUF_SIZE];
			StringBuilder builder = new StringBuilder();
			for(int numRead=in.read(charBuf,0,CHARBUF_SIZE); numRead>0 && numRead<=CHARBUF_SIZE; numRead=in.read(charBuf,0,CHARBUF_SIZE)){
				for(int i=0;i<numRead;i++){
					builder.append((char) charBuf[i]);
				}
			}
			String inputText = builder.toString();
			
			if(inputText.equals("")){
				System.out.println("[DEBUG] Array size error or site was empty.");
				return null;
			}
			else return inputText;
		}
		catch(SocketTimeoutException e){ //if the internet is so slow it times out
			System.out.println("[DEBUG] Connection timed out.");
			return null;
		}
		catch(IOException e){
			System.out.println("[DEBUG] Error reading from site. "+e.getMessage());
			return null;
		}
	}

    public static Object getJSONFromURL(String siteURL,String args,boolean isPost){
		String siteString="";
		try{
			if(isPost){
				URL urlObj = new URL(siteURL);
				siteString = getStringFromPOSTURL(urlObj,args);
			}
			else{
				URL urlObj = new URL(siteURL+"?"+args);
				siteString = getStringFromGETURL(urlObj);
			}
			if(siteString==null) return null;
			else if(siteString.equals("")) return new JSONArray();
			
			JSONParser parser = new JSONParser();
			Object JSONData = parser.parse(siteString);
			return JSONData;
		}
		catch(MalformedURLException e){ //if the URL isn't valid
			System.out.println("[DEBUG] Invalid URL: "+siteURL);
			return null;
		}
		catch(ParseException e){ //if the website JSON isn't formatted correctly
			System.out.println("[DEBUG] Failed to parse site JSON. Site contained: "+siteString);
			return null;
		}
    }
	
	public static Object getJSONFromURL(String siteURL,String args){
		return getJSONFromURL(siteURL,args,false); //use get by default
	}
}
