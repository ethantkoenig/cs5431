<#macro master title="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>EzraCoinL Wallet</title>

    <!------------------------------------------- CSS ------------------------------------------>

    <!-- Latest Bootstrap compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

    <!-- Multi-select css -->
    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/multi-select/0.9.12/css/multi-select.min.css">

    <!-- index page css -->
    <link rel="stylesheet" type="text/css" href="/css/index.css">


    <!------------------------------------------- JS ------------------------------------------>

    <!-- JQuery -->
    <script src=" https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>

    <!-- Latest compiled and minified Bootstrap JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
            integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
            crossorigin="anonymous"></script>

    <!-- Multi-select js -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/multi-select/0.9.12/js/jquery.multi-select.min.js"></script>

    <script src="/js/sjcl.js"></script>
    <script src="/js/bn.js"></script>
    <script src="/js/ecc.js"></script>
    <script src="/js/index.js"></script>

    <link rel="shortcut icon" type="image/x-icon" href="/png/icon.png">

</head>
<body>
<!-- Navigation -->
<header class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse"
                    data-target="#bs-example-navbar-collapse-1">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">EzraCoinL Wallet</a>
        </div>
        <!-- Collect the nav links, forms, and other content for toggling -->
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">
                <#if loggedIn>
                    <li>
                        <a href="/user">${loggedInUsername}</a>
                    </li>
                    <li>
                        <a href="/balance">Balance</a>
                    </li>
                    <li>
                        <a href="/transact">Transact</a>
                    </li>
                    <li>
                        <a href="/requests">Requests</a>
                    </li>
                    <li>
                        <a href="/change_password">Change Password</a>
                    </li>
                    <li>
                        <a href="#" id="logout">Logout</a>
                    </li>
                <#else>
                    <li>
                        <a href="/register">Register</a>
                    </li>
                    <li>
                        <a href="/login">Login</a>
                    </li>
                </#if>

            </ul>
        </div>

        <!-- /.navbar-collapse -->
    </div>
    <!-- /.container -->
</header>
    <#if success??>
    <div class="container" style="padding-top: 10px;">
        <div class="alert alert-success">
            <strong>Success!</strong> ${success}
        </div>
    </div>
    </#if>
    <#if alert??>
    <div class="container" style="padding-top: 10px;">
        <div class="alert alert-warning">
            <strong>Alert!</strong> ${alert}
        </div>
    </div>
    </#if>
    <#if error??>
    <div class="container" style="padding-top: 10px;">
        <div class="alert alert-danger">
            <strong>Error!</strong> ${error}
        </div>
    </div>
    </#if>
<div>
    <div>
        <#nested/>
    </div>
</div>

</body>
</html>
</#macro>