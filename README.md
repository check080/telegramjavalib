# telegramjavalib
An easy java interface for telegram bots.

# Tutorial
A simple application looks like the following:
`class Master{
	
	private static TelegramBot tg;
	
	public static void main(String[] args){
		TelegramBot tg = new TelegramBot(...); //Put a bot API key here
		tg.sendMessage(...,"Sup"); //Put a chat ID here (or a user ID for private messages)
	}
}`
