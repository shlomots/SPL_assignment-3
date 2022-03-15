
#ifndef CLIENT_OPERATION_H
#define CLIENT_OPERATION_H


#include <string>
#include <vector>
#include <boost/lexical_cast.hpp>


class Operation{
private:
    short opcode;
    std::string message;
    std::vector<std::string> arguments; //arguments[0] always contains the opcode
    std::string sendingTime;


public:
    void setArguments(std::string &input);//gets the op arguments as string and splits them by the ' 'chat to the string arguments vector
    void setOpcodeByArg();
    short getOpcode() const;
    const std::vector<std::string> &getArguments() const;
    std::string getSendingTime();
    void setOpcode(int opcode);
    std::string& getMessage();
    void setMessage(std::string message);

    virtual ~Operation()=default;

    Operation(std::string userInput, std::string sendingTime);
    explicit Operation();
};

#endif //CLIENT_OPERATION_H
