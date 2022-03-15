#ifndef CONNECTION_HANDLER__
#define CONNECTION_HANDLER__
                                           
#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include "OperationEncoderDecoder.h"

using boost::asio::ip::tcp;

class ConnectionHandler {
private:
    const std::string host_;
    const short port_;
    boost::asio::io_service io_service_;
    tcp::socket socket_;

public:
    ConnectionHandler(std::string host, short port);
    virtual ~ConnectionHandler();

    bool connect();
    bool getLine(Operation& op);
    short getShort(std::string& str, int counter);
    bool getString(std::string& stringToWrite);
    bool decodeNotification(Operation& readOp);
    bool decodeAck(Operation& readOp);
    bool decodeError(Operation& readOp);
    bool getBytes(char bytes[], unsigned int bytesToRead);
    bool sendBytes(const char bytes[], int bytesToWrite);
    bool sendOp(Operation& op);
    void close();

};
 
#endif