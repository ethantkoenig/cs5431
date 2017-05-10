<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row padding-top">
        <div>
            Confirm key upload for key ${publickey}
        </div>
        <div class="col-md-4 col-md-offset-4">
            <form action="/keys/add" method="post">
                <input type="hidden" name="guid" value="${guid}">
                <div class="row text-center">
                    <input class="btn btn-primary submit-button" type="submit" value="Upload key">
                </div>
            </form>
        </div>
    </div>
</div>


</div>
<!-- /.container -->
</@layout.master>

