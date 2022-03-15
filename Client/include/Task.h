
#ifndef CLIENT_TASK_H
#define CLIENT_TASK_H

#include <thread>

/**
 * Runnable Interface Replacement
 */

class Task {
protected:
    virtual int run()=0;
    virtual ~Task()=default;

};


#endif //CLIENT_TASK_H
