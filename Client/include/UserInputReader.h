
#ifndef CLIENT_USERINPUTREADER_H
#define CLIENT_USERINPUTREADER_H


#include "Task.h"
#include "connectionHandler.h"
#include "Operation.h"
#include "OperationEncoderDecoder.h"

class UserInputReader: public Task {
private:
    ConnectionHandler &clientConnectionHandler;
    std::atomic_bool &terminate;
    std::atomic_bool &loggedIn;

public:
    UserInputReader(ConnectionHandler &clientConnectionHandler,std::atomic_bool &terminate,std::atomic_bool &loggedIn);
    int run(); ////the "main" method to run in the thread
};


#endif //CLIENT_USERINPUTREADER_H
