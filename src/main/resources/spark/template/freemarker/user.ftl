<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${loggedInUser}</h1>
        </div>
    </div>


    <div class="row">
        <div class="col-sm-6 padding-top">

            <form method="post" action="/keys" id="keyform" style="padding-top: 20px;">
                <div class="form-group">

                    <div class="checkbox">
                        <label><input title="Generate new key" type="checkbox" id="keyform-generate">Generate new
                            key</label>
                    </div>
                </div>
                <div class="form-group publickey-form-group">
                    <input type="text" class="form-control" placeholder="Public Key">
                </div>
                <input type="hidden" name="publickey">
                <div class="form-group privatekey-form-group">
                    <input type="text" class="form-control" placeholder="Private Key">
                </div>
                <input type="hidden" name="privatekey">
                <div class="form-group password-form-group">
                    <input type="password" class="form-control" placeholder="Password">
                </div>
                <input type="hidden" class="hidden-password" name="password">
                <input class="btn btn-primary" type="submit" value="Update">
            </form>
            <div class="row" id="status-message"></div>
        </div>
        <div class="col-sm-6 padding-top">
            <select id='friend-selector' multiple='multiple'>
            </select>
        </div>

        <#list users as user>
            <#if user != loggedInUser>
                <script>
                    $('#friend-selector').append($("<option/>", {
                        value: "${user}",
                        text: "${user}",
                    }));
                </script>
            </#if>
        </#list>

        <#list friends as friend>
            <script>
                $('#friend-selector')
                        .append($("<option></option>")
                                .attr("value", "${friend}")
                                .text("${friend}")
                                .attr('selected', 'selected'));

            </script>
        </#list>


    </div>
</div>
<!-- /.container -->
</@layout.master>
