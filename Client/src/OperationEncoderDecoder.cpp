
#include "../include/OperationEncoderDecoder.h"

#include <utility>

//should return the number of bytes to write
int OperationEncoderDecoder::encode(Operation op, char *bytes) {
    OperationEncoderDecoder::shortToBytes(op.getOpcode(), bytes); //encoding opcode to the bytes array
    int writeCurrPosition = 2; //where to continue writing, after 2 bytes of opcode
    switch (op.getOpcode()) {
        case 1: //username, password, birthday
        case 2: //username, password, captcha
        {
            for (unsigned int i = 1; i <= 3; ++i) { //add all 3 arguments to the bytes array
                stringToBytes(op.getArguments()[i],bytes,writeCurrPosition);
                writeCurrPosition += op.getArguments()[i].length();
                bytes[writeCurrPosition] = '\0';
                writeCurrPosition++;
            }
            return writeCurrPosition;
        }

        case 3: //no arguments
        case 7:
        {return writeCurrPosition;}
        case 4: //follow/unfollow, username
        {
            for (unsigned int i = 1; i <= 2; ++i) { //add all 3 arguments to the bytes array
                stringToBytes(op.getArguments()[i],bytes,writeCurrPosition);
                writeCurrPosition += op.getArguments()[i].length();
            }
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            return writeCurrPosition;
        }

        case 5: //the content of the post are in different arguments. add them all and then put the \0
        {
            for (unsigned int i = 1; i < op.getArguments().size(); ++i) {
                stringToBytes(op.getArguments()[i],bytes,writeCurrPosition);
                writeCurrPosition += op.getArguments()[i].length();
                bytes[writeCurrPosition] = ' ';
                writeCurrPosition++;
            }
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            return writeCurrPosition;
        }
        case 6: //username, the content of the message, sending time
        {
            stringToBytes(op.getArguments()[1],bytes,writeCurrPosition);
            writeCurrPosition += op.getArguments()[1].length();
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            for (unsigned int i = 2; i < op.getArguments().size(); ++i) {
                stringToBytes(op.getArguments()[i], bytes, writeCurrPosition);
                writeCurrPosition += op.getArguments()[i].length();
                bytes[writeCurrPosition] = ' ';
                writeCurrPosition++;
            }
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            stringToBytes(op.getSendingTime(),bytes,writeCurrPosition);
            writeCurrPosition += op.getSendingTime().length();
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            return writeCurrPosition;
        }
        case 8:
        case 12:
        {
            stringToBytes(op.getArguments()[1],bytes,writeCurrPosition);
            writeCurrPosition += op.getArguments()[1].length();
            bytes[writeCurrPosition] = '\0';
            writeCurrPosition++;
            return writeCurrPosition;
        }



    }
    return -1;
}

void OperationEncoderDecoder::shortToBytes(short num, char* bytesArr)
{
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

void OperationEncoderDecoder::stringToBytes(std::string arg, char *bytes, int currPosition) {
    for (unsigned int i = 0; i < arg.length(); ++i) {
        bytes[currPosition+i]=arg.at(i); //add each char of the string to the bytes array
    }
}

short OperationEncoderDecoder::bytesToShort(char* bytesArr){
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}