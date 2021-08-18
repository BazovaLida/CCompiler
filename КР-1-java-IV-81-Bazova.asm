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

main proc
	push ebp
	mov ebp, esp
	push 10
	pop [ebp-12];	var1_var
	push 17
	pop [ebp-16];	var2_var
	push [ebp-12]     ;var1_val
pop eax	;if
cmp eax, [ebp-16]
jle _L2

	push [ebp-12]     ;var1_val
	pop [ebp-20];	max_var
	jmp _L3
_L2:
	push [ebp-16]     ;var2_val
	pop [ebp-20];	max_var
_L3:
	push [ebp-20]     ;max_val
pop eax	;if
cmp eax, 0
jge _L4

	push [ebp-20]     ;max_val
	pop EBX
	neg EBX
	push EBX

	pop [ebp-20];	max_var
_L4:
	push 0
	pop [ebp-24];	sum_var
	push 1
	pop [ebp-28];	i_var
_for_start1:
	mov ebx, [ebp-28]
	sub ebx, 1
	push [ebp-20]     ;max_val
	pop eax	;expression
	cmp eax, ebx
	jle _for_end1	;if value less than expr

	push [ebp-20]     ;max_val
	push [ebp-28]     ;i_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D0
	mov edx, 0ffffffffh
	jmp _D1
_D0:
	mov edx, 0
_D1:
	idiv ECX
	push EDX

pop eax	;if
cmp eax, 0
jne _L6

	push [ebp-24]     ;sum_val
	push [ebp-28]     ;i_val
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop [ebp-24];	sum_var
_L6:
	push [ebp-28]     ;i_val
	push 1
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop [ebp-28];	i_var
	jmp _for_start1
_for_end1:
	push [ebp-24]     ;sum_val
	pop eax ;here is the result
	mov esp, ebp	;restore ESP
	pop ebp	;restore old EBP
	ret 0

main ENDP
END start
