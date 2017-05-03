<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/unlock/mail" method="post">
                <div class="form-group">
                    <input type="text" class="form-control" name="email" placeholder="Email">
                </div>
                <input class="btn btn-primary submit-button" type="submit" value="Send Recovery Email">
            </form>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>

