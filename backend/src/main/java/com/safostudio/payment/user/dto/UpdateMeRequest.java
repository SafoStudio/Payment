package com.safostudio.payment.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeRequest {

  @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Invalid phone number format")
  private String phone;

  @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
  private String firstName;

  @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
  private String lastName;
}
