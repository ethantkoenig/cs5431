<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <div class="row">
                <form action="/login" method="post">
                    <div class="form-group">
                        <input type="text" class="form-control" id="usernameInput" name="username"
                               placeholder="Username">
                    </div>
                    <div class="form-group">
                        <input type="password" class="form-control" id="passwordInput" name="password"
                               placeholder="Password">
                    </div>
            </div>
            <div class="row text-center">
                <input class="btn btn-primary submit-button" type="submit" value="Login">
            </div>
            <div class="row text-center" style="padding-top: 10px;">
            <#-- TODO: The reletive path here and throughout is bad. Need to figure out how the template works with paths to fix-->
                <a href="../recover" class="show_hide">Forgot password?</a>
            </div>
        </div>
        </form>
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


</div>
<!-- /.container -->
</@layout.master>

