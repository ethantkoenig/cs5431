<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${username}</h1>
        </div>
    </div>


    <div class="row">
        <#if loggedIn && loggedInUsername == username>
            <div class="col-sm-6 padding-top">

                <form method="post" action="/user/keys" id="keyform" style="padding-top: 20px;">
                    <div class="form-group">
                        <label>
                            <input title="Generate new key" type="checkbox" id="keyform-generate">Generate new key
                        </label>
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
            </div>
            <div class="col-sm-6 padding-top">
                <select id='friend-selector' multiple='multiple'>
                </select>
            </div>
        </#if>

        <#if users??>
            <#list users as user>
                <#if user != username>
                    <script>
                        $('#friend-selector').append($("<option/>", {
                            value: "${user}",
                            text: "${user}",
                        }));
                    </script>
                </#if>
            </#list>
        </#if>

        <#if friends??>
            <#list friends as friend>
                <script>
                    $('#friend-selector')
                            .append($("<option></option>")
                                    .attr("value", "${friend}")
                                    .text("${friend}")
                                    .attr('selected', 'selected'));

                </script>
            </#list>
        </#if>


    </div>
</div>
<!-- /.container -->
</@layout.master>
