#include "../include/connectionHandler.h"
 
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;

ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(), socket_(io_service_){}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " << host_ << ":" << port_ << std::endl;
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getLine(Operation& op) {
    std::string stringOpcode;
    short opcode = getShort(stringOpcode, 2); //get the opcode
    if (opcode==-1){
        return false;
    }
    op.setOpcode(opcode);
    switch (opcode) {
        case 9:
        {return decodeNotification(op);}
        case 10:
        {return decodeAck(op);}
        case 11:
        {return decodeError(op);}
    }
    return false;
}

short ConnectionHandler::getShort(std::string &str, int counter) {
    char ch;
    int readCounter=0;
    try{
        do {
            if (getBytes(&ch, 1)){
                str.append(1, ch);
                readCounter++;
            } else
                return -1;
        } while (readCounter<counter);
    }catch (std::exception& e){
        return -1;
    }
    char toShort[2]; //turn this bytes to short
    if (counter==2){
        toShort[0]= str[0];
        toShort[1] = str[1];
    } else{
        toShort[0]=0;
        toShort[1]=str[0];
    }
    return OperationEncoderDecoder::bytesToShort(toShort);
}



bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
            tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getString(std::string &stringToWrite) {
    char ch;
    try {
        do {
            if (getBytes(&ch, 1)){
                if (ch!= '\0')
                    stringToWrite.append(1,ch);
                else
                    return true;
            } else
                return false;
        } while (ch!= '\0');
    } catch (std::exception e){
        return false;
    }
    return true;
}

bool ConnectionHandler::decodeAck(Operation &readOp) {
    std::string message = "ACK ";
    std::string toWrite;
    short ackType = getShort(toWrite, 2); //get the message opcode- what type of ack is this
    toWrite = std::to_string(ackType);
    message+= toWrite;
    toWrite= ""; //clear content
    switch (ackType) {
        case 1:
        case 2:
        case 3:
        case 5:
        case 6:
        case 9:
        case 12:
            readOp.setMessage(message);
            break;
        case 4:
            if (getString(toWrite)) {
                message += " " + toWrite;
                readOp.setMessage(message);
            }
            else
                return false;
            break;
        case 7: //LOGSTAT
        case 8://STAT
            for (unsigned int i = 0; i < 4; ++i) { //get age, numPosts, numFollowers, numFollowing
                short whatEver = getShort(toWrite, 2); //get the message opcode- what type of ack is this
                message+=" ";
                message+= std::to_string(whatEver);
                toWrite= ""; //clear content
            }
            char ch;
            try {
                if (getBytes(&ch,1)){
                    if (ch!=';'){ //it is not the end of the message
                        message+= "\n"; //there is another line like this
                        readOp.setMessage(message); //set this line to the message
                        getLine(readOp); //continue reading the next line. recursive
                    } else
                        readOp.setMessage(message); //add this line
                } else
                    return false;
            }catch (std::exception& e){
                return false;
            }break;

    }
    return true;
}

bool ConnectionHandler::decodeNotification(Operation &readOp) {
    std::string message = "NOTIFICATION ";
    std::string toWrite;
    short notificationType = getShort(toWrite, 1); //get notification type
    if (notificationType==-1){
        return false;
    }
    if (notificationType==0L){
        message+="PM ";
    } else{
        message+= "Public ";
    }
    toWrite= ""; //clearing this string so we continue to write on this
    if (getString(toWrite)) //get posting user
        message+= toWrite + " ";
    else
        return false;
    toWrite= ""; //clearing this string so we continue to write on this
    if (getString(toWrite)) //get content;
        message+= toWrite;
    else
        return false;
    readOp.setMessage(message);
    return true;
}

bool ConnectionHandler::decodeError(Operation &readOp) {
    std::string message = "ERROR ";
    char ch;
//    int readCounter=0;
//    try{
//        do {
//            if (getBytes(&ch, 1)){
//                message.append(1, ch);
//                readCounter++;
//                std::cout <<"set the counter"<< std::endl;
//            } else
//                return -1;
//        } while (readCounter<2);
//    }catch (std::exception& e){
//        return false;
//    }
    std::string toWrite;
    short ackType = getShort(toWrite, 2); //get the message opcode- what type of ack is this
    toWrite = std::to_string(ackType);
    message+= toWrite;
    toWrite= ""; //clear content
    readOp.setMessage(message);
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendOp(Operation &op) {
    char bytesBuff[1024];
    int bytesToWrite = OperationEncoderDecoder::encode(op, bytesBuff); //encode Operation byteBuffer
    if(bytesToWrite!=-1)
    {
        return sendBytes(bytesBuff,bytesToWrite); //write the encoded operation to the socket for sending
    }
    return false;
}


// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}
