<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <div class="row">
                <form action="/register" method="post" id="registerform">
                    <div class="form-group">
                        <input type="text" class="form-control" name="username" placeholder="Username">
                    </div>
                    <div class="form-group">
                        <input type="text" class="form-control" name="email" placeholder="Email">
                    </div>
                    <div class="form-group password-form-group">
                        <input type="password" class="form-control" placeholder="Password">
                    </div>
                    <div class="form-group confirm-form-group">
                        <input type="password" class="form-control" placeholder="Password">
                    </div>
                    <input type="hidden" class="hidden-password" name="password">
                    <div class="text-center">
                        <input class="btn btn-primary submit-button" type="submit" value="Register">
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>

