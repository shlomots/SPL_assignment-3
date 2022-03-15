
#include <stdlib.h>
#include <thread>
#include "../include/connectionHandler.h"
#include "../include/UserInputReader.h"


int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    std::atomic_bool terminate(false); //know when the client terminated
    std::atomic_bool loggedIn(false);

    UserInputReader inputReader(connectionHandler, terminate, loggedIn); //User Input Handling Task
    std::thread inputThread(&UserInputReader::run, &inputReader); //User Input Handling Task Thread

    while (!terminate) {
        Operation readOp;
        if (!connectionHandler.getLine(readOp)) { // getting answer operation from server (bytes to Operation decoding included)
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }

        std::cout << readOp.getMessage() << std::endl; //printing to user end

        if(readOp.getOpcode()==10){ //ack message, check if it was login or logout
            if (readOp.getMessage().at(4)=='2'){//login
                loggedIn=true;
            }

            else if (readOp.getMessage().at(4)=='3'){ //logout
                terminate=true;
                inputThread.join(); //waiting for it to terminate
            }
        }
    }
    return 0;
}
