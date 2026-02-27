package com.safostudio.payment.wallet.graphql;

import com.safostudio.payment.user.service.dto.UserResponse;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WalletDataLoader {

    private final WalletRepository walletRepository;

    @BatchMapping(field = "wallets", typeName = "User")
    public Map<UserResponse, List<Wallet>> wallets(List<UserResponse> users) {
        if (users.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<UUID> userIds = users.stream()
                .map(UserResponse::getId)
                .collect(Collectors.toSet());

        List<Wallet> wallets = walletRepository.findAllByUserIdIn(userIds);

        Map<UUID, List<Wallet>> walletsByUserId = wallets.stream()
                .collect(Collectors.groupingBy(Wallet::getUserId));

        Map<UserResponse, List<Wallet>> result = new HashMap<>();
        for (UserResponse user : users) {
            result.put(user, walletsByUserId.getOrDefault(user.getId(), new ArrayList<>()));
        }

        return result;
    }
}