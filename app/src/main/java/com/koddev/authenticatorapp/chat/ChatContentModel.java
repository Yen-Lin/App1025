package com.koddev.authenticatorapp.chat;

public class ChatContentModel
{
    public String sentence;
    public String ismine;

    public ChatContentModel(){

    }


    public ChatContentModel(String sentence,String ismine)
    {
        this.sentence = sentence;
        this.ismine = ismine;
    }

    public String getsentence()
    {
        return sentence;
    }

    public void setsentence(String sentence)
    {
        this.sentence = sentence;
    }

    public String getismine()
    {
        return ismine;
    }

    public void setismine(String ismine)
    {
        this.ismine = ismine;
    }
}
