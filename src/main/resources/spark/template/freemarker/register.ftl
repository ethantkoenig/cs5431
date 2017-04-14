<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <div class="row">
                <form action="/register" method="post">
                    <div class="form-group">
                        <input type="text" class="form-control" id="usernameInput" name="username"
                               placeholder="Username">
                    </div>
                    <div class="form-group">
                        <input type="text" class="form-control" id="emailInput" name="email"
                               placeholder="Email">
                    </div>
                    <div class="form-group">
                        <input type="password" class="form-control" id="passwordInput" name="password"
                               placeholder="Password">
                    </div>
                    <div class="text-center">
                        <input class="btn btn-primary submit-button" type="submit" value="Register">
                    </div>
                </form>
            </div>
        </div>
    </div>
    <#if success??>
        <div class="row" style="padding-top: 10px;">
            <div class="col-md-4 col-md-offset-4">
                <div class="alert alert-success">
                    <strong>Success!</strong> ${success}
                </div>
            </div>
        </div>
    </#if>
    <#if error??>
        <div class="row" style="padding-top: 10px;">
            <div class="col-md-4 col-md-offset-4">
                <div class="alert alert-danger">
                    <strong>Error!</strong> ${error}
                </div>
            </div>
        </div>
    </#if>

</div>
<!-- /.container -->
</@layout.master>

