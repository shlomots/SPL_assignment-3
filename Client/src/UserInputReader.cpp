
#include "../include/UserInputReader.h"

UserInputReader::UserInputReader(ConnectionHandler &clientConnectionHandler,std::atomic_bool &terminate,std::atomic_bool &loggedIn) : clientConnectionHandler(clientConnectionHandler), terminate(terminate), loggedIn(loggedIn) {}

int UserInputReader::run() {
    while (true)
    {
        const short bufsize = 1024;
        char buf[bufsize]; //creating char array that will hold the keyboard input stream of chars
        time_t now = time(0);
        std::cin.getline(buf, bufsize); //reading an entire line of input stream of chars and stores it in the buf char array
        tm *thisTime = localtime(&now);
        std::string sendingTime = std::to_string(thisTime->tm_mday) + '-' + std::to_string(thisTime->tm_mon+1) + '-' + std::to_string(thisTime->tm_year+1900)
                                  + ' ' + std::to_string(thisTime->tm_hour) + ':' + std::to_string(thisTime->tm_min);
        std::string line(buf); //creating String obj from char array
        try {
            Operation userCommand(line, sendingTime);
            userCommand.setArguments(userCommand.getMessage());
            userCommand.setOpcodeByArg();

            if (!clientConnectionHandler.sendOp(userCommand)) { // passing operation ref to connectionHandler for sending
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
            if(userCommand.getOpcode() == 3 && loggedIn) // checking if logout had been sent and if client already logged in
            {
                //if so, wait for termination signal from main thread
                while (!terminate){std::this_thread::yield();}
                break;
            }
        } catch (std::exception& e) //expecting in case of connection problem or non valid command from user
        {
            std::cerr << e.what() << std::endl;
        }

    }
    return 0;
}
