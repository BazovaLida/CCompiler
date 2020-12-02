.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

multiply proc
	push ebp
	mov ebp, esp
	mov eax, [ebp+12]
	mov [ebp-16], eax	;b_var
	mov eax, [ebp+8]
	mov [ebp-12], eax	;a_var
;multiply_param 
	push 0
	pop [ebp-20];	result_var
	push 0
	pop [ebp-24];	i_var
_for_start1:
	mov ebx, [ebp-24]
	sub ebx, 1
	push [ebp-12]     ;a_val
	pop eax	;expression
	cmp eax, ebx
	jle _for_end1	;if value less than expr

	push [ebp-20]     ;result_val
	push [ebp-16]     ;b_val
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop [ebp-20];	result_var
	push [ebp-24]     ;i_val
	push 1
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop [ebp-24];	i_var
	jmp _for_start1
_for_end1:
	push [ebp-20]     ;result_val
	pop eax ;here is the result
	mov esp, ebp	;restore ESP
	pop ebp	;restore old EBP
	ret 2

multiply endp
main proc
	push ebp
	mov ebp, esp
	push 5
	push 5
call multiply
	push eax
	pop eax ;here is the result
	mov esp, ebp	;restore ESP
	pop ebp	;restore old EBP
	ret 0

main ENDP
END start
