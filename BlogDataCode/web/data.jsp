<%--
    Document   : index
    Created on : 17-Feb-2010, 09:44:19
    Author     : ads
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <link rel="stylesheet" type="text/css" href="css/gen1.css"/>
        <link rel="stylesheet" type="text/css" href="css/gen2.css"/>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Godiva Blog</title>
    </head>
    <body>
        <div class="containerPost">
            <h2><div class="postTitle">Add Data</div></h2>
            <h3><a href="index.jsp">Add Post</a></h3>

            <div>
                <dl>
                    <dt>Your OpenID: ${identifier}</dt>
                </dl>
            </div>

            <div class="postText">
                <form method=POST action="/ncWMS/data">
                    <br/>Title: <br/><input type="text" name="btitle" size="95"><br><br>
                    <input type="file" name="bfile" value="" width="50" /><input type="reset" value="Clear" name="clearbutton" />
                    <br/><br/><input type="submit" value="Submit">
                </form>
            </div>
        </div>
    </body>
</html>
