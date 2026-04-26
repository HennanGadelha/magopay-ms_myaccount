package com.magopay.my_account.core.application.ports.in;

import com.magopay.my_account.core.application.ports.in.query.FindUserByEmailQuery;
import com.magopay.my_account.core.application.ports.in.result.FindUserByEmailResult;

public interface FindUserByEmailUseCase {
    FindUserByEmailResult execute(FindUserByEmailQuery query);
}

