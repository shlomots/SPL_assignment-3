
#ifndef CLIENT_OPERATIONENCODERDECODER_H
#define CLIENT_OPERATIONENCODERDECODER_H

#include "Operation.h"
#include <boost/asio.hpp>
#include <iostream>

class OperationEncoderDecoder {
private:
    static void shortToBytes(short num, char* bytesArr);
    static void stringToBytes(std::string arg, char* bytes, int currPosition);

public:
    static int encode(Operation op, char bytes[]);
    static short bytesToShort(char* bytesArr);
};

#endif //CLIENT_OPERATIONENCODERDECODER_H
