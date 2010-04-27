<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>Login Success!</title>
        <link rel="stylesheet" type="text/css" href="css/gen1.css"/>
        <link rel="stylesheet" type="text/css" href="css/gen2.css"/>
        <meta http-equiv="Content-Type" content="application/xhtml-xml; charset=utf-8" />
    <body>
        <div class="containerPost">

            <div class="postText">
                <h2><div class="postTitle">Add Data</div></h2>
                <form method=POST action="/ncWMS/data">
                    <br/>Title: <br/><input type="text" name="dtitle" size="95"><br><br>
                    <input type="file" name="dfile" value="" width="50" /><input type="reset" value="Clear" name="clearbutton" />
                    <br/><br/><input type="submit" value="Submit">
                </form>

                <h2><div class="postTitle">Add Post</div></h2>
                <form method=POST action="/ncWMS/blog">
                    <br/>Your OpenID:<br/><input type="text" name="bopenid" size="50" value= ${identifier}><br>
                    <br/>Title: <br/><input type="text" name="btitle" size="95"><br>
                    <br/>Some Text: <br/><textarea name="btext" rows="5" cols="50"></textarea><br>
                    <br/>Id(add data first): <br/><input type="text" name="bid" size="25" value=""><br>
                    <br/><br/><input type="submit" value="Submit">
                </form>
            </div>

            <div>
                <a href="logout.jsp">Logout</a>
            </div>
        </div>
    </body>
</html>
