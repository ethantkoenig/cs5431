<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row">
        <div class="col-md-6">
            <form action="/requests" method="post">
                <div class="form-group">
                    <label>Requestee</label>
                    <select class="form-control" name="requestee" id="recipient-selector">
                    </select>
                </div>
                <div class="form-group">
                    <label>Amount</label>
                    <input type="number" class="form-control" name="amount">
                </div>
                <div class="form-group">
                    <label>Message</label>
                    <textarea id="message" maxlength="50" class="form-control" rows="4" name="message"></textarea>
                </div>
                <input id="request-button" class="btn btn-primary" type="submit" data-url="/requests"
                       value="Request EzraCoinL">
            </form>
        </div>
        <div class="col-md-6">
            <#list requests as request>
                <div class="well">
                    <form action="/transact" method="post" class="transactform">
                        <h2>${request.toUser} has requested $${request.amount}.</h2>
                        <p>${request.message}</p>
                        <div class="horizontal">
                            <div class="input-group col-md-8">
                                <input type="hidden" name="recipient" value="${request.toUser}">
                                <input type="hidden" name="tranId" value="${request.tranId?c}">
                                <input type="hidden" name="amount" value="${request.amount?c}">
                                <input type="hidden" name="message" value="${request.message}">
                                <input type="password" class="form-control transaction-password" placeholder="Password">
                                <span class="input-group-btn">
                                    <button class="btn btn-secondary btn-success" type="submit">Accept</button>
                                </span>
                            </div>
                            <div class="input-group">
                                <button class="btn btn-secondary btn-danger delete-request" data-tranId=${request.tranId?c}>
                                    Delete
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </#list>
            <div class="row" id="status-message"></div>
        </div>
    </div>

    <#if friends?? && friends?has_content>
        <#list friends as friend>
            <script>
                $('#recipient-selector').append($('<option>', {value: "${friend}", text: "${friend}"}));
            </script>
        </#list>
    <#else>
        <script>
            $('#recipient-selector').append('<option value="" disabled>You have not authorized anyone to send you money.</option>');
        </script>
    </#if>

</div>
<!-- /.container -->
</@layout.master>

