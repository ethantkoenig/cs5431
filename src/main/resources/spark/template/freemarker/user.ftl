<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${username}</h1>
            <p>
            Public keys:
            <#list hashes as hash>
                ${hash}
            </#list>
            </p>
        </div>
    </div>
    <!-- /.row -->

</div>
<!-- /.container -->
</@layout.master>
