<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/change_password/mail" method="post">
                <input class="btn btn-primary submit-button" type="submit" value="Change my password">
            </form>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>

