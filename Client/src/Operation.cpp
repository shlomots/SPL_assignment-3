
#include "../include/Operation.h"

#include <utility>

Operation::Operation(std::string userInput, std::string sendingTime) : opcode(0), message(std::move(userInput)), arguments() ,sendingTime(std::move(sendingTime)){
}

Operation::Operation() : opcode(0), message(){}

void Operation::setArguments(std::string &input) {
    unsigned long curr=0;
    for (unsigned long i=0; i < input.length(); i=curr) {
        curr= input.find_first_of(' ', i); //find the index of the next ' '
        if (curr==-1){
            arguments.push_back(input.substr(i));
            break;
        }
        arguments.push_back(input.substr(i, curr - i)); //add the argument sub stringed from the last position for 'cur-last' chars
        curr++; //jump over the ' ' before new cycle
    }
}

void Operation::setOpcodeByArg() {
    if (arguments[0]=="REGISTER"){
        opcode = 1;
    } else if (arguments[0]=="LOGIN"){
        opcode = 2;
    } else if (arguments[0]=="LOGOUT"){
        opcode = 3;
    } else if (arguments[0]=="FOLLOW"){
        opcode = 4;
    }else if (arguments[0]=="POST"){
        opcode = 5;
    }else if (arguments[0]=="PM"){
        opcode = 6;
    }else if (arguments[0]=="LOGSTAT"){
        opcode = 7;
    }else if (arguments[0]=="STAT"){
        opcode = 8;
    }else if (arguments[0]=="BLOCK"){
        opcode = 12;
    }
}

short Operation::getOpcode() const {
    return opcode;
}

const std::vector<std::string> &Operation::getArguments() const {
    return arguments;
}

std::string Operation::getSendingTime(){
    return sendingTime;
}

void Operation::setOpcode(int opcode) {
    this->opcode = opcode;
}

std::string &Operation::getMessage() {
    return message;
}

void Operation::setMessage(std::string message) {
    this->message.append(message);
}





