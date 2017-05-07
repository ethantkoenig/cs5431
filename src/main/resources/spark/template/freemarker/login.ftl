<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/login" method="post" id="loginform">
                <div class="row">
                    <div class="form-group">
                        <input type="text" class="form-control" name="username" placeholder="Username">
                    </div>
                    <div class="form-group password-form-group">
                        <input type="password" class="form-control" placeholder="Password">
                    </div>
                    <input type="hidden" class="hidden-password" name="password">
                </div>
                <div class="row text-center">
                    <input class="btn btn-primary submit-button" type="submit" value="Login">
                </div>
                <div class="row text-center" style="padding-top: 10px;">
                <#-- TODO: The reletive path here and throughout is bad. Need to figure out how the template works with paths to fix-->
                    <a href="../reset" class="show_hide">Forgot password?</a>
                </div>
            </form>
        </div>
    </div>
</div>


</div>
<!-- /.container -->
</@layout.master>

