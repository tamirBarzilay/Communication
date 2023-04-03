#pragma once
#include <string>
#include <iostream>
#include "../include/ConnectionHandler.h"
#include "../include/summarize.h"
#include "../include/event.h"
#include <map>
#include <vector>
#include <thread>
#include <mutex>
#include <sstream>
#include <fstream>
// TODO: implement the STOMP protocol
class StompProtocol
{
private:
std::map<int,std::map<std::string,int>> receipt_waiting;
std::map<std::string,int> topic_subscription;
std::map<std::string,std::map<std::string,summarize>> summarize_map;
int subId;
int receiptId;
string user_name;
bool loggedIn;
ConnectionHandler &connectionHandler;
int disconnect_receipt_id;
std::mutex  & _mutex;
public:
StompProtocol(ConnectionHandler &connectionHandler, std::mutex & _mutex);

bool process(std::string &command);
bool process_answer(std::string &answer);
bool send_reports(names_and_events & file);
};
