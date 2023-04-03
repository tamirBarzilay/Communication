
#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include <thread>
#include <mutex>
#include <iostream>
#include "../include/StompProtocol.h"
using std::string;

int main(int argc, char *argv[])
{
    bool connected = true;
    while(connected){
    const short bufsize = 1024;
    char buf[bufsize];
    // reading command line from keyboard
    //while not terminated
    std::cin.getline(buf, bufsize);
    std::string line(buf);
    std::stringstream s(line);
    std::string segment;
    std::vector<std::string> login_command_vec;
    while (std::getline(s, segment, ' '))
    {
        login_command_vec.push_back(segment);
    }

    string host_port = login_command_vec.at(1);
    std::string host = host_port.substr(0, host_port.find(':'));
    short port = stoi(host_port.substr(host_port.find(':') + 1));

    ConnectionHandler connectionHandler(host, port);

    if (!connectionHandler.connect())
    {
        std::cerr << "Could not connect to server" << host << ":" << port << std::endl;
        return 1;
    }
    

    std::mutex _mutex;
    StompProtocol protocol(connectionHandler, _mutex);
    bool terminate_keyboard_thread = false;
    bool logOut = false;
//creating a thread to get input from the server
    std::thread thread_server_input([&connectionHandler, &logOut, &protocol,&terminate_keyboard_thread,&connected]() {
    while (connected)
    {
        string answer;
        if (!connectionHandler.getLine(answer))
        {
            std::cout << "Disconnected. Exiting...\n"
                      << std::endl;
            logOut = true;
            terminate_keyboard_thread=true;
            connected = false;
            break;
        }
        
        if (!protocol.process_answer(answer))//process answer returns false
        {                                    // if a receipt for a disconnect frame was received or an error frame
            std::cout << "Disconnected. Exiting...\n"
                      << std::endl;
            logOut = true;
            terminate_keyboard_thread = true;
            break;
        }
    } });
    thread_server_input.detach();

    protocol.process(line);//sending login frame to the server

    //main thread gets input from keyboard as log as the server-client connection is active
    while (!terminate_keyboard_thread) //while logged in
    {
        const short bufsize = 1024;
        char buf[bufsize];
        // reading from keyboard
        std::cin.getline(buf, bufsize);
        if(terminate_keyboard_thread){
            break;
            }
        std::string line(buf);
        bool error = protocol.process(line);
        if (error == true)
        {
            std::cout << "Disconnected. Exiting...\n"
                      << std::endl;
            connected = false;
            break;
        }
        if (line == "logout")
        { // client waits for receipt after logout frame was sent
            while (!logOut) ;
            if (logOut)
                terminate_keyboard_thread = true;
            logOut = false;
        }
    }
}
    return 0;
}