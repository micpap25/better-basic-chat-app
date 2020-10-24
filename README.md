# better-basic-chat-app

ChatClient:
 Messages in this section will be represented by "*"
*: sends out a chat to the rest of the clients, except for to the client itself (doesn't echo the message back)
@username *: sends a private message to the requestedusername
/quit: leaves the server

Special feature: 
Room names in this section will be represented by "*"
Chat rooms can now be created
/list: lists all room names
/join *: puts the client in room *.
/leave: removes the client from room *.
/name * allows the user to rename themselves.

ChatGuiClient:
Takes the user to a page to type the IP Address and Port number
Prompts the user to type a valid username, then welcomes and allows the user to send messages
Displays an updated version of who is in the server
/list: lists all room names
/join *: puts the client in room *.
/leave: removes the client from room *.